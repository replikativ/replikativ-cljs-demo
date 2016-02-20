# replikativ-cljs-demo

This is a small demo project of how to use
[replikativ](https://github.com/replikativ/replikativ) in the Browser. See [replikativ-demo](https://github.com/replikativ/replikativ-demo) for the corresponding Clojure project.

Have a look at the `core.cljs` namespace of how to change and track
the state of a `CDVCS` repository.

## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

Optionally (e.g. in Emacs with CIDER):

Connect to nREPL on port 7888 and
Paste the following into the REPL to hook into the figwheel cljs REPL,
```clojure
(require 'figwheel-sidecar.repl-api)
(figwheel-sidecar.repl-api/cljs-repl)
```


To clean all compiled files:

    lein clean

## License

Copyright © 2016 Christian Weilbach

This project is released to the public domain, no rights reserved.
