#! /bin/bash
curl -v -H "SBW-Key:DATA123456" -H "SBW-Timestamp:1294513200" -H "SBW-Signature:54fc7ffd3cdc856c09c8747b61718741166f347b93f43c8db2ce6e4f568881e1" http://127.0.0.1:8080/clearinghouse/events/test
