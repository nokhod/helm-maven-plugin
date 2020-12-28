# install gsed for mac
gsed -e ':a;$!{N;ba}; s/\n*\s*<profile>\n*\s*<id>test<\/id>\(.\)*<\/profile>//' $1
