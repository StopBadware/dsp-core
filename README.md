# DSP-CORE

## Environment Variables
* SBW_IMP_HOST
* SBW_IMP_SECRET
* SBW_RES_HOST
* SBW_RES_SECRET
* SBW_IMPORT_HOURS
* SBW_RESOLVE_HOURS
* MAX_AUTH_AGE
* MONGO_URL
* MONGO_MAX_CONN
* MONGO_CONN_TIMEOUT_SEC
* MONGO_SOCK_TIMEOUT_SEC
* IRON_MQ_PROJECT_ID
* IRON_MQ_TOKEN

## MongoDB Indexes
### event_reports
* {"reversed_host":1}
* {"host":1}
* {"is_on_blacklist":1}
* {"prefix":1}
* {"report_type":1}
* {"reported_at":1}
* {"scheme":1}
* {"path":1}
* {"query":1}
* {"_created":1}
* {"sha2_256":1,"prefix":1,"reported_at":-1},{unique:true}

### hosts
* {"host":1},{unique:true}
* {"ips.ip":1},{sparse:true}

### ips
* {"ip":1},{unique:true}
* {"asns.asn":1},{sparse:true}

### autonomous_systems
* {"asn":1},{unique:true}
* {"name":1}
* {"country":1}

### participants
* {"prefix":1},{unique:true}
* {"display_as":1},{unique:true}
* {"full_name":1}

### accounts
* {"api_key":1},{unique:true}

### roles
* {"role":1},{unique:true}
