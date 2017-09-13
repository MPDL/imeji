node {
  	stage ('Checkout'){
	   // Checkout code from repository
	   checkout scm
	}

	stage ('Build'){	
		// Build with maven
		withMaven(jdk: 'Java 8', maven: 'M339') {
		   sh 'mvn -Dmaven.test.failure.ignore=true clean install'		   
		}
	}
	
   	stage ('Deploy'){
	   	echo "We are currently working on branch: ${env.BRANCH_NAME}"
	   
	    switch (env.BRANCH_NAME){
	    	case 'dev':
	    		echo "Deploy to dev";
		   		sshagent(['29b4ac97-0fcf-4aa5-b592-70da4a50f995']) {
				   sh "scp target/imeji.war dev-imeji.mpdl.mpg.de:/var/lib/tomcat8/webapps"
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