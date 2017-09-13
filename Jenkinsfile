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
		   		sshagent(['e0807011-204d-4901-ad8f-323c45e84d29']) {
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