# What determines the price of a diamond?

This project demonstrates xvsy plotting using the [diamonds](http://docs.ggplot2.org/0.9.3.1/diamonds.html) dataset from ggplot2.

## Prerequisites

Java. Ideally, you also have leiningen.

## Running

You may use either leiningen or download a java uberjar.

#### Using leiningen (highly recommended)
Using leiningen enables automatic code-reloading.

```
git clone https://github.com/dvdt/xvsy.git
cd xvsy && lein install
cd example/diamonds && lein ring server
```

#### Java

```
wget http://davetsao.com/xvsy/diamonds-0.1.0-SNAPSHOT-standalone.jar
PORT=3000 java -cp diamonds-0.1.0-SNAPSHOT-standalone.jar clojure.main -m diamonds.handler
```

## Play around

##### React-based web GUI for data exploration: [http://localhost:3000](http://localhost:3000)

##### Embeddable web GUI: [http://localhost:3000/embed](http://localhost:3000/embed)

Example plots are served from:
- [http://localhost:3000/plot-1](http://localhost:3000/plot-1)
- [http://localhost:3000/plot-2](http://localhost:3000/plot-2)
- [http://localhost:3000/plot-3](http://localhost:3000/plot-3)
- [http://localhost:3000/plot-4](http://localhost:3000/plot-4)

Try editing the plots in [diamonds.handler](/example/diamonds/src/diamonds/handler.clj). If you're
running `lein ring server`, code changes you make are reloaded live.


Copyright © 2015 David Tsao
