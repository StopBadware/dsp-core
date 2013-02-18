#! /bin/bash
curl -v -H "sbw_key:DATA123456" -H "sbw_ts:1360786590" -H "sbw_sig:FOOBAR" http://127.0.0.1:8080/clearinghouse/events/test
