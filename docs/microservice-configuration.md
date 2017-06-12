# Microservice(s) Configuration

# Sample
```
{

  "artifact" : {
    "name" : "microservice",
    "version" : "1.0-SNAPSHOT",
    "scope" : "user"
  },
  "config" : {
    "version" : 1,
    "id" : "GeoFenceNotifier",
    "description" : "A Microservice for sending notification on entry and exist of a geo fence.",
    "plugin" : {
      "name" : "abc",
      "version" : "1.1-SNAPSHOT",
      "scope" : "user"
    }
    "configuration" : {
      "instances" : 1,
      "vcores" : 1,
      "memory" : 512,
      "endpoints" : {
        "in" : [
          "ms://ericsson.com/geofence"
        ],
        "out" : [
          "ms://ericsson.com/notify"
        ]
      },
      "properties" : {
        "a" : "b",
        "c" : "d"
      }
    }
  }
}
```