FROM amazon/aws-lambda-java:11.2022.11.03.13

# Copy function code and runtime dependencies from Gradle layout
COPY lucene-index/ ${LAMBDA_TASK_ROOT}/lucene-index/
COPY build/dependency/* ${LAMBDA_TASK_ROOT}/lib/
COPY build/resources/main ${LAMBDA_TASK_ROOT}
COPY build/classes/java/main ${LAMBDA_TASK_ROOT}

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "art.kittencat.lambda.Handler" ]
