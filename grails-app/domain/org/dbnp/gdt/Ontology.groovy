package org.dbnp.gdt
import org.apache.commons.logging.LogFactory
/**
 * This class describes an existing ontology, of which terms can be stored (actually 'cached' would be a better description)
 * in the (global) Term store.
 * This information is mapped from the BioPortal NCBO REST service, e.g.: http://rest.bioontology.org/bioportal/ontologies/38802
 * see http://www.bioontology.org/wiki/index.php/NCBO_REST_services
 *
 * Revision information:
 * $Rev: 1174 $
 * $Author: work@osx.eu $
 * $Date: 2010-11-19 10:55:15 +0100 (Fri, 19 Nov 2010) $
 */
class Ontology implements Serializable {
	String name             // BioPortal: displayLabel
	String description      // BioPortal: description
	String url              // BioPortal: homepage
	String versionNumber    // BioPortal: versionNumber
	int ncboId              // BioPortal: ontologyId
	int ncboVersionedId     // BioPortal: id
	Date dateCreated
	Date lastUpdated

    static mapping = {
        description(type: 'text')
    }

	static constraints = {
		ncboId(unique: true)     // For now, we just want one version of each NCBO ontology in the database
	}	

	/**
	 * Find child terms
	 * @return A set containing all terms that reside under this ontology
	 */
	Set<Term> giveTerms() {
		Term.findAllByOntology(this)
	}

	Object giveTermByName(String name) {
		giveTerms().find {
			it.name == name
		}

		/* TODO: find out why the following doesn't work (probably more efficient):
		Term.find {
			it.name == name
			it.ontology == this
		}
		}*/
	}

	static Ontology getBioPortalOntology(int ncboId) {
		getBioPortalOntology(ncboId as String)
	}
	static Ontology getBioPortalOntology(String ncboId) {
		// Get ontology from BioPortal via Ontocat
		// TODO: maybe make a static OntologyService instance to be more efficient, and decorate it with caching?
		uk.ac.ebi.ontocat.OntologyService os = new uk.ac.ebi.ontocat.bioportal.BioportalOntologyService()
		uk.ac.ebi.ontocat.Ontology o = os.getOntology(ncboId)
		
		// Instantiate and return Ontology object
		if (o != null) {
			new Ontology(
				name: o.label,
				description: o.description,
				url: o.properties['homepage'] ?: "http://bioportal.bioontology.org/ontologies/${o.id}",
				versionNumber: o.versionNumber,
				ncboId: o.ontologyAccession,
				ncboVersionedId: o.id
			);
		} else {
			println ("ERROR: ontology with ncboId ${ncboId} could not be found!")
		}
	}
    static List<uk.ac.ebi.ontocat.OntologyTerm> searchBioPortalOntologyTerms(String term) {
        // Get ontology from BioPortal via Ontocat
        // TODO: maybe make a static OntologyService instance to be more efficient, and decorate it with caching?
        uk.ac.ebi.ontocat.OntologyService os = new uk.ac.ebi.ontocat.bioportal.BioportalOntologyService()
        List<uk.ac.ebi.ontocat.OntologyTerm> o = os.searchAll(term)
        return o;
    }
    static List<uk.ac.ebi.ontocat.OntologyTerm> searchTermWithinBioPortalOntology(String term, String ontology) {
        // Get ontology from BioPortal via Ontocat
        // TODO: maybe make a static OntologyService instance to be more efficient, and decorate it with caching?
        uk.ac.ebi.ontocat.OntologyService os = new uk.ac.ebi.ontocat.bioportal.BioportalOntologyService()
        List<uk.ac.ebi.ontocat.OntologyTerm> o = os.searchOntology(ontology, term)
        return o;
    }


	/**
	 * Return the Ontology by ncboId, or create it if nonexistent.
	 * @param ncboId
	 * @return Ontology
	 */
	static Ontology getOrCreateOntologyByNcboId( String ncboId ) {
		return getOrCreateOntologyByNcboId( ncboId as int )
	}
	static Ontology getOrCreateOntologyByNcboId( int ncboId ) {
		def ontology = findByNcboId( ncboId as String )

		// got an ontology?
		if (!ontology) {
			// no, fetch it from the webservice
			ontology = getBioPortalOntology( ncboId )

			if (ontology && ontology.validate() && ontology.save(flush:true)) {
				ontology.refresh()
			}
		}

		return ontology
	}

	static Ontology getBioPortalOntologyByTerm(String termId) {
		// Get ontology from BioPortal via Ontocat
		// TODO: maybe make a static OntologyService instance to be more efficient, and decorate it with caching?
		uk.ac.ebi.ontocat.OntologyService os = new uk.ac.ebi.ontocat.bioportal.BioportalOntologyService()
		uk.ac.ebi.ontocat.OntologyTerm term = os.getTerm( termId );
		uk.ac.ebi.ontocat.Ontology o = os.getOntology( term.getOntologyAccession() );

		// Instantiate and return Ontology object
		new Ontology(
			name: o.label,
			description: o.description,
			url: o.properties['homepage'] ?: "http://bioportal.bioontology.org/ontologies/${o.id}",
			versionNumber: o.versionNumber,
			ncboId: o.ontologyAccession,
			ncboVersionedId: o.id
		);
	}

	/**
	 * Instantiate Ontotology class by searching the web service for (versioned)id.
	 * @param	ontologyId (bioportal versionedId)
	 * @return	ontology instance
	 */
	static Ontology getBioPortalOntologyByVersionedId(String ncboVersionedId) {
		try {
			// use the NCBO REST service to fetch ontology information
			def url = "http://rest.bioontology.org/bioportal/ontologies/" + ncboVersionedId
			def xml = new URL(url).getText()
			def data = new XmlParser().parseText(xml)
			def bean = data.data.ontologyBean

			// instantiate Ontology with the proper values
			def ontology = new Ontology(
				name: bean.displayLabel.text(),
				description: bean.description.text(),
				url: bean.homepage.text(),
				versionNumber: bean.versionNumber.text(),
				ncboId: bean.ontologyId.text() as int,
				ncboVersionedId: bean.id.text() as int
			)

			// validate ontology
			if (ontology.validate()) {
				// proper instance
				return ontology
			} else {
				// it does not validate
				throw new Exception("instantiating Ontology by (versioned) id [" + ncboVersionedId + "] failed")
			}
		} catch (Exception e) {
			// whoops?!
			LogFactory.getLog(this).error e
			return null
		}
	}
}
