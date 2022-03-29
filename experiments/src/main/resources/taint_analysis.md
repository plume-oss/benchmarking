# fastjson
### 2 paths

- Servlet request in a logger debug (2 flows)
```
com.alibaba.fastjson.support.spring.JSONPResponseBodyAdvice.beforeBodyWrite:65
com.alibaba.fastjson.support.spring.JSONPResponseBodyAdvice.beforeBodyWrite:70
```

# guava
### 11 paths

- System property entering a java.io.File constructor (4 flows)
```
com.google.common.base.StandardSystemProperty.value:158
com.google.common.reflect.ClassPath.parseJavaClassPath:645
```

- System property being logged (4 flows)
```
com.google.common.base.StandardSystemProperty.value:158
com.google.common.reflect.ClassPath.parseJavaClassPath:650
```

- Memory output (ByteArrayOutputStream) being written directly to FileOutputStream (3 flows)
```
com.google.common.io.FileBackedOutputStream.update:236
com.google.common.io.FileBackedOutputStream.update:236
```

# guice
### 0 paths

# jackson-core
### 8 paths

- raw JSON token text written to string sinks (2 paths)
```
com.fasterxml.jackson.core.JsonGenerator.copyCurrentEvent:2452
com.fasterxml.jackson.core.JsonGenerator.copyCurrentEvent:2454
```

- False positives associated with over-tainting the above (6 paths)
```
com.fasterxml.jackson.core.JsonGenerator.copyCurrentEvent:2452
com.fasterxml.jackson.core.JsonGenerator.copyCurrentEvent:2454
```

# mockito
### 0 paths

# mybatis-3
### 12 paths

- Printing raw SQL results and metadata to print writer (8 flows)
```
org.apache.ibatis.jdbc.ScriptRunner.printResults:293
org.apache.ibatis.jdbc.ScriptRunner.println:312
```

- False positive exception from obtaining SQL result set interpreted as influencing SQL exception message (4 flows)
```
org.apache.ibatis.jdbc.ScriptRunner.printResults:293
org.apache.ibatis.jdbc.ScriptRunner.println:312
```

# rxjava
### 0 paths

# scribejava
### 12 paths

- Response body written to logs during debug mode (12 flows)
```
com.github.scribejava.core.oauth.OAuth20Service.sendAccessTokenRequestSync:154
com.github.scribejava.core.oauth.OAuth20Service.sendAccessTokenRequestSync:154
```

```
com.github.scribejava.core.oauth.OAuth20Service.getDeviceAuthorizationCodes:552
com.github.scribejava.core.oauth.OAuth20Service.getDeviceAuthorizationCodes:552
```
```
com.github.scribejava.core.oauth.OAuth10aService.getRequestToken:44
com.github.scribejava.core.oauth.OAuth10aService.getRequestToken:45
```
```
com.github.scribejava.core.oauth.OAuth20Service.getAccessTokenDeviceAuthorizationGrant:620
com.github.scribejava.core.oauth.OAuth20Service.getAccessTokenDeviceAuthorizationGrant:620
```
# spring-boot
### 10 flows

- False positive where output of a System.getProperty caused exception thought to influence what is printed later (4 flows)
```
org.springframework.boot.system.SystemProperties.get:33
org.springframework.boot.system.SystemProperties.get:40
```

- Value of a system-defined variable given to File constructor (6 flows)
```
org.springframework.boot.system.SystemProperties.get:34
org.springframework.boot.web.context.WebServerPortFileWriter.<init>:78
```
