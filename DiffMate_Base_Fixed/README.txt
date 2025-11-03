How to compile and run
----------------------
Requires Java 8+ and Gson on the classpath.

Example:
  javac -cp gson-2.10.1.jar -d out $(find src -name "*.java")
  java -cp out:gson-2.10.1.jar com.mmd.json.CompareJsonFiles ref.json new.json ./reports ./config.json OptionalReportBaseName