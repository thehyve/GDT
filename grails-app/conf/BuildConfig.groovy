grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        grailsRepo "http://grails.org/plugins"

        mavenCentral()
        mavenRepo "http://nexus.dbnp.org/content/repositories/releases"
        mavenRepo "http://ontocat.sourceforge.net/maven/repo"
        mavenRepo "http://repository.springsource.com/maven/bundles/release"
        mavenRepo "http://repository.springsource.com/maven/bundles/external"
        mavenRepo "http://repository.springsource.com/maven/libraries/release"
        mavenRepo "http://repository.springsource.com/maven/libraries/external"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        runtime("uk.ac.ebi:ontoCAT:0.9.8") {
		transitive = false
	}
    }

    plugins {
        build(  ":tomcat:$grailsVersion",
                ":release:2.2.1",
                ":rest-client-builder:latest.integration"
        ) {
            // plugin only plugin, should not be transitive to the application
            export = false
        }

        compile(":hibernate:$grailsVersion",
		":jquery:latest.integration",
		":webflow:2.0.0",
		":ajaxflow:latest.integration"
        ) {
            export = false
        }
    }
}
