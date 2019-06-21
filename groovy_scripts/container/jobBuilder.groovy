#!groovy
/*
Document: Extract job information from k8s_jobs.txt. Generate kubernetes cronjob yaml. 
*/

def jobFileName = "./k8s_jobs.txt"
def jobManifest = new File(jobFileName)

//def regex = /^(\S.*\*+?)\s+JOB_NAME=(\S.*)\s+(cd\s.*)/
def regex = /^(\S.*\*+?)\s+JOB_NAME=(\S.*?)\s(.*?)$/
def jobPattern = ~regex

def jobTemlateFile = new File("./cronJobTemplate.yaml")
def targetPath = "./jobs"
def imageTag = "IMAGE_TAG"

def jobGenerator(jobFile, pattern, templateFile, targetFilePath, currentBranch) {
    if ( !new File(targetFilePath).exists() ) {
        new File(targetFilePath).mkdirs()
    }
    jobFile.eachLine { line->
        if (( matcher = line =~ pattern )) {
            jobList = [ jobName: matcher[0][2].replaceAll("::","-").toLowerCase(), cronTime: matcher[0][1], jobCommand: matcher[0][3], imageTag: "$currentBranch"]
            templateEngine = new groovy.text.GStringTemplateEngine()
            converteFile = templateEngine.createTemplate(templateFile).make(jobList)
            File targetFile = new File("${targetFilePath}/${matcher[0][2].replaceAll("::","-")}.yaml")
            targetFile.text = converteFile.toString()
        }
    }
}

jobGenerator(jobManifest, jobPattern, jobTemlateFile, targetPath, imageTag)