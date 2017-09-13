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
		   		sshagent(credentials: ['59cb9a3a-7463-44b4-befe-457eac3bd014']) {
		   		   sh 'echo SSH_AUTH_SOCK=$SSH_AUTH_SOCK'
       			   sh 'ls -al $SSH_AUTH_SOCK || true'
				   sh "scp -vvv -o StrictHostKeyChecking=no target/imeji.war saquet@dev-imeji.mpdl.mpg.de:/var/lib/tomcat8/webapps"
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