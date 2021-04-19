#!/usr/bin/env python3

import sys
import json
import re
import logging

xxxxx = "xxxxx"


def cleanup_input():
    for line in sys.stdin:
        try:
            json_line = json.loads(line)
            clean_line = cleanup_json_line(json_line)
            print(json.dumps(clean_line))
        except Exception:
            # It's not JSON serializable
            line = cleanup_string_line(line)
            print(line, end="")


# '.traces[0][0].trace_id |= "xxxxxx" |
#                                  .traces[0][0].span_id |= "xxxxxx" |
#                                  .traces[0][0].start |= 1234567890 |
#                                  .traces[0][0].duration |= 12345'

def cleanup_json_line(json_line: object):
    if "traces" in json_line:
        # almost certainly a set of traces from dd-trace-java
        # traces should be a [][]spans
        for trace in json_line["traces"]:
            for span in trace:
                if "trace_id" in span:
                    span["trace_id"] = xxxxx
                if "span_id" in span:
                    span["span_id"] = xxxxx
                if "start" in span:
                    span["start"] = 1234567890
                if "duration" in span:
                    span["duration"] = 12345
                if "meta" in span:
                    if "function_arn" in span["meta"]:
                        arn = span["meta"]["function_arn"]
                        account_pattern = r":\d{12}:"
                        arn = re.sub(account_pattern, ":123456789012:", arn)
                        span["meta"]["function_arn"] = arn
                    if "runtime-id" in span["meta"]:
                        span["meta"]["runtime-id"] = "00000000-0000-0000-0000-000000000000"
                    if "request_id" in span["meta"]:
                        span["meta"]["request_id"] = "00000000-0000-0000-0000-000000000000"
    elif "e" in json_line:
        # probably a metric
        json_line["e"] = 1234567890
    return json_line


# 2021-03-30 03:35:44 <96a98bc2-951c-469b-99cd-a3eee60fdcfc> INFO  com.serverless.Handler:19 - received:
def cleanup_string_line(line: str):
    ts_pattern = r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(:\d{3})?"
    line = re.sub(ts_pattern, "1970-01-01 00:00:00", line)

    guid_pattern = r"[0-9a-f-]{36}"
    line = re.sub(guid_pattern, "00000000-0000-0000-0000-000000000000", line)
    return line


if __name__ == "__main__":
    logging.info("main started")
    cleanup_input()
