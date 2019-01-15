#!/bin/bash
for f in $(egrep -o -R "defn?-? [^ ]*" src --include '*.cljs' | cut -d \  -f 2 | sort | uniq); do
    echo $f $(grep -R --include '*.cljs' -- "$f" src | wc -l);
done | grep " 1$"
