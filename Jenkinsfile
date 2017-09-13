pipeline {
	 tools {
        maven 'M3'
        jdk 'Java 8'
    }
  	stages {
	  	stage ('Checkout'){
		   // Checkout code from repository
		   checkout scm
		}
	
		stage ('Build'){	
			// Build with maven
		    sh "mvn clean install"
		}
		
	   	stage ('Deploy'){
		   	echo "We are currently working on branch: ${env.BRANCH_NAME}"
		   
		    switch (env.BRANCH_NAME){
		    	case 'dev':
		    		echo "Deploy to dev";
		    		break;
		    	case 'qa':
		    		echo "deploy to qa";
		    		break;
		    	default:
		    		echo "no deployment";
		    }
		}
	}
}