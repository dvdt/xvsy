X<i>vs</i>Y: Grammar of graphics plotting, implemented in Clojure and React
===
**x***vs***y** is a [ggplot2](http://ggplot2.org/) inspired tool for
  visualizing, exploring and analyzing datasets. It is designed to
  handle even very large datasets with hundreds of columns.  It may be
  accessed via a browser-based graphical interface, a JSON api, or
  from within clojure.

Play around with these **x***vs***y** visualizations:
- [20 million US flights, from 2012-2015](http://davetsao.com/flights-visualizer.html)
- [2 years of daily logs for 40,000 Backblaze hard drives](http://davetsao.com/backblaze-hdd-visualizer.html)

Also read the [blog post on finding the best frequent flyer program](http://davetsao.com/blog/2015-06-01-simple-easy-data-viz.html)
for a walk-through analysis.

Clojure plotting examples
---

### The heavier the diamond, the lighter the wallet
###### Also, people like round numbers
```clojure
(qspec :diamonds :point
       :aes [(x CARAT)
             (fill "white")
             (color "steelblue")
             (size 2)
             (y PRICE :id)]
       :where [["<" :CARAT 3]])
```
![](http://davetsao.com/xvsy/plot-1.svg.gz)

### Price per carat of a diamond dramatically increases at 1 carat
###### For the best deal, buy a 0.95 carat diamond
```clojure
(qspec :diamonds :dodged-bar
       :aes [(x CARAT :bin :lower 0 :upper 5.5 :nbins 55)
             (y (non-factor "AVG(PRICE / CARAT)") :sql)])
```
![](http://davetsao.com/xvsy/plot-3.svg.gz)

Quick Start
---
Start a xvsy plotting server on your machine to explore the diamonds dataset. Detailed instructions with in the [diamonds example project](./example/diamonds/README.md)

### Using Leiningen
```
git clone https://github.com/dvdt/xvsy.git
cd xvsy && lein install && cd example/diamonds && lein ring server
```

### Alternatively, just use java
Download and run an uberjar
```
wget http://davetsao.com/xvsy/diamonds-0.1.0-SNAPSHOT-standalone.jar
PORT=3000 java -cp diamonds-0.1.0-SNAPSHOT-standalone.jar -m clojure.main
```

Now open [http://localhost:3000](http://localhost:3000) in your favorite web browser.

Philosophy
---
At it's core, xvsy is:

1. A domain specific language (i.e. grammar of graphics) for declaratively specifying plots
2. A grammar of graphics to SQL translator for executing plot queries against a database.
 - Special care is taken to ensure plotting semantics can easily be translated to map/reduce.
 - Ideal for column oriented databases.
 - Tested against H2 and Google Big Query. Other databases supported
   by [Korma]() like Postgres and MYSQL should also work.
3. A SVG based plot generator for query results.

When using the grammar of graphics writer's block (data scientist's
block?) can be addressed by writing *syntactically* correct but
*semantically* meaningless statements, just to get the creative juices
flowing. I've implemented a browser-based UI (in React) to facilitate forming
plot specifications. It is designed to make syntax errors impossble--even a rat
randomly clicking on the screen will always generate valid plots on the
underlying data.


Customizable Plots with Chrome Developer Tools
---
Changing colors, font sizes, etc. is interactive and simple! In Chrome, right-click `Inspect Element` over the plot element to be modified and attach your own CSS or custom SVG.

Everything is alpha! Features may be incomplete.
---
I started this side project to 1) learn the grammar of graphics and 2)
learn clojure. Along the way, I also started using
[React](https://facebook.github.io/react/). Nothing here was ever
meant to be production code. Use at your own risk.


Copyright © 2015 David Tsao
