node {
	def JAVA_HOME = tool name: 'Java 8', type: 'jdk'
	def  mvnHome = tool name: 'Maven35', type: 'maven'

  	stage ('Checkout'){
	   // Checkout code from repository
	   checkout scm
	}

	stage ('Build'){	
		// Build with maven
		sh("${mvnHome}/bin/mvn clean install")	  
	}
	
   	stage ('Deploy'){
	   	echo "We are currently working on branch: ${env.BRANCH_NAME}"
	    switch (env.BRANCH_NAME){
	    	case 'dev':
	    		echo "Deploy to dev";
		   		sshagent(['26045cb2-b6f5-4f07-8261-70a2f2e22860']) {
				   sh "scp target/imeji.war tomcat8@dev-imeji.mpdl.mpg.de:/var/lib/tomcat8/webapps"
				}
	    		break;
	    	case 'qa':
	    		echo "deploy to qa";
	    		break;
	    	default:
	    		echo "no deployment";
	    }
	}

}