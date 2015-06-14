# diamonds

Example app for demonstrating xvsy plotting library

## Prerequisites

You will need java installed


## Running

You may use either leiningen or download a java uberjar.

#### Using leiningen (highly recommended)
```lein ring server```

#### Java

```
wget http://davetsao.com/diamonds.jar
java -cp diamonds.jar clojure.main -m diamonds.handler
```

## Play around
[http://localhost:3000](http://localhost:3000) is the React-based web GUI for plot generation.

Example plots are also at:
- [http://localhost:3000/plot-1](http://localhost:3000/plot-1)
 - ![plot result](http://localhost:3000/plot-3)
- ![http://localhost:3000/plot-2](text.svg)
- [http://localhost:3000/plot-3](http://localhost:3000/plot-3)

Try editing the plots [src/handler.clj](src/handler.clj). If you're
running `lein ring server`, code changes you make are updated live.

## License
EPL

Copyright © 2015 David Tsao
