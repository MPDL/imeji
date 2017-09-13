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
		   		sshagent(['a3e9e7ed-94a4-4fb9-ba79-8864a382b684']) {
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