node {
	env.JAVA_HOME = tool name: 'OpenJDK 11', type: 'jdk'
	def  mvnHome = tool name: 'Maven35', type: 'maven'


	echo "We are currently working on branch: ${env.BRANCH_NAME}"
	echo "JDK installation path is: ${env.JAVA_HOME}"

  	stage ('Checkout'){
	   // Checkout code from repository
	   checkout scm
	}

	stage ('Build'){	
		// Build with maven
		sh("${mvnHome}/bin/mvn clean install -P env-testing")	  
	}
	
   	stage ('Deploy'){
	    switch (env.BRANCH_NAME){
	    	case 'dev':
	    		echo "deploy to dev";
		   		sshagent(['26045cb2-b6f5-4f07-8261-70a2f2e22860']) {
				   sh "scp target/imeji.war tomcat8@dev-imeji.mpdl.mpg.de:/srv/web/tomcat9/webapps"
				}
	    		break;
	    	case 'qa':
	    		echo "deploy to qa";
	    		sshagent(['26045cb2-b6f5-4f07-8261-70a2f2e22860']) {
				   sh "scp target/imeji.war tomcat8@qa-imeji.mpdl.mpg.de:/srv/web/tomcat9/webapps"
				}
	    		break;
	    	case 'openjdk11':
	    		echo "deploy to dev with tomcat9 / openjdk 11";
	    		sshagent(['26045cb2-b6f5-4f07-8261-70a2f2e22860']) {
				   sh "scp target/imeji.war tomcat8@dev-imeji.mpdl.mpg.de:/srv/web/tomcat9/webapps"
				}
	    		break;
	    	default:
	    		echo "no deployment";
	    }
	}

}