node {
	env.JAVA_HOME = tool name: 'Java JDK 21', type: 'jdk'
	def  mvnHome = tool name: 'Maven390', type: 'maven'


	echo "We are currently working on branch: ${env.BRANCH_NAME}"
	echo "JDK installation path is: ${env.JAVA_HOME}"

  	stage ('Checkout'){
	   // Checkout code from repository
	   checkout scm
	}

	stage ('Build'){	
		try	{
			// Build with maven
			sh("${mvnHome}/bin/mvn clean install -P env-testing")	  
		}
		finally {
			// Capture test reports
			junit '**/target/surefire-reports/*.xml'
		}
	}
	
   	stage ('Deploy'){
	    switch (env.BRANCH_NAME){
	    	case 'dev':
	    		echo "deploy to dev";
		   		sshagent(['26045cb2-b6f5-4f07-8261-70a2f2e22860']) {
				   sh "scp target/imeji.war tomcat8@dev-imeji.mpdl.mpg.de:/srv/web/tomcat10/webapps"
				}
	    		break;
	    	case 'qa':
	    		echo "deploy to qa";
	    		sshagent(['26045cb2-b6f5-4f07-8261-70a2f2e22860']) {
				   sh "scp target/imeji.war tomcat8@qa-imeji.mpdl.mpg.de:/srv/web/tomcat10/webapps"
				}
	    		break;
	    	default:
	    		echo "no deployment";
	    }
	}

}