/**
 * TermEditorController Controller
 *
 * Webflow driven term editor
 *
 * @author  Jeroen Wesbeek
 * @since	20100420
 * @package	studycapturing
 *
 * Revision information:
 * $Rev: 1457 $
 * $Author: work@osx.eu $
 * $Date: 2011-01-31 14:14:35 +0100 (Mon, 31 Jan 2011) $
 */
package org.dbnp.gdt

class TermEditorController {
	/**
	 * index closure
	 */
    def index = {
		// got a ontology get parameter?
		def ontologies = (params.ontologies) ? params.ontologies : null

		// enter the flow!
    	redirect(action: 'pages', params:["ontologies":ontologies])
    }

	/**
	 * Webflow
	 */
	def pagesFlow = {
		// start the flow
		onStart {
			log.info ".start term / ontology editor flow"

			if (params.ontologies) {
				flow.ontologies		= params.ontologies
				flow.ontologiesList	= []
				params.ontologies.split(/\,/).each() { ncboId ->
					// trim the id
					ncboId.trim()

					// and add to the flow scope
					flow.ontologiesList[ flow.ontologies.size() ] = ncboId
				}
			}
		}

		// main term editor page
		terms {
			render(view: "terms")
			onRender {
				log.info ".rendering term selection popup"
			}
			on("add") {
				def ncboId = params.int('term-ncbo_id')
				def ncboVersionedId = params.get('term-ontology_id')
				def ontology = null

				if( ncboVersionedId == "null" )
					ncboVersionedId = null
				
				try {
					// got the ncboId?
					if (ncboId) {
						// find ontology by ncboId
						ontology = Ontology.findByNcboId(ncboId as int)
					} else if (ncboVersionedId && ncboVersionedId.isNumber() ) {
						// find ontology by ncboId
						ontology = Ontology.findByNcboVersionedId(ncboVersionedId as int)
					} else if( !ncboVersionedId ){
						// somehow we didn't get both the ncboId as well
						// as the versioned id. Throw an error.
						throw new Exception("We did not receive the ontology with your request, please try again")
					}

					// do we have the ontology?
					if (!ontology) {
						// no, try to instantiate by using the BioPortal
						ontology = (ncboId && ncboId != "null") ? Ontology.getBioPortalOntology( ncboId as int ) : Ontology.getBioPortalOntologyByVersionedId( ncboVersionedId as String )

						// validate and save ontology
						if (!(ontology && ontology.validate() && ontology.save(flush:true))) {
							if (ncboId && ncboId != "null") {
								throw new Exception("An Ontology with ncboId ${ncboId} (= Ontology ID) does not seem valid. See http://bioportal.bioontology.org/ontologies")
							} else {
								throw new Exception("An Ontology with ncboVersionedId ${ncboVersionedId} (= URL id) does not seem valid. See http://bioportal.bioontology.org/ontologies")
							}
						}
					}

					// instantiate term with parameters
					def term = new Term(
						name: params.get('term'),
						ontology: ontology,
						accession: params.get('term-concept_id')
					)

					// validate term
					if (term.validate()) {
						// save the term to the database
						if (term.save(flush:true)) {
							flash.message = "'" + params.get('term') + "' was successfully added, either search for another term to add or close this window"
							success()
						} else {
							flash.errors = ["We encountered a problem while storing the selected term. Please try again."]
							//term.errors.each() { println it }
							error()
						}
					} else {
						// term did not validate properly
						if (term.errors =~ 'unique') {
							flash.errors = ["'" + params.get('term') + "' already exists, either search for another term or close this window"]
						} else {
							flash.errors = ["We encountered a problem while storing the selected term. Please try again."]
						}

						error()
					}
				} catch (Exception e) {
					flash.errors = ["${e.getMessage()}"]

					error()
				}
			}.to "terms"
		}
	}
}