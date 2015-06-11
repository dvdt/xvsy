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

Quick Start
---
Start a xvsy plotting server on your machine to begin exploring the [diamonds]() dataset.

### Using Leiningen
```
git clone asdasf
cd xvsy && lein install && cd example/diamonds && lein run -m diamonds.handler
```

### Alternatively, just use java
There's also an uberjar packaged
```
java -cp xvsy/examples/diamonds.jar -m clojure.main
```

Now open [http://localhost:3333](http://localhost:3333) in your favorite web browser.

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
