### ServerListener

Android app to track your own server scripts and bots for updates (as notifications).

Typical usecase: you have server with Cron script. This script can form statistics and error reports at regular time intervals. ServerListener connects to server every N minutes. If there is some new information, it throws a notification.

English and Russian languages inside.

##### JSON calls reference (simple as far as possible)

See working server examples in `json-api.lisp` (Common Lisp) and `notification-add.sh`.

###### User request

POST (or GET) query: `ts_id=<integer>` (0 by default)

###### Server response:

```
{"current_ts_id":6, "notifications":[
	{"title": "your_title_1", "text": "My new stats"},
	{"title": "title2", "text": "My second stats"}]}
```

where current_ts_id - tracking ID for updates.
