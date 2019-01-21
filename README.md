# Effect-ive Clojure

Pure functions are the building blocks of idiomatic Clojure(Script) programs, since we can easily understand and test them. By definition, these functions should not have any effects i.e. should not change the outside world. However, our programs are only useful if they produce effects like sending emails, displaying information, or saving information to databases.

This talk explores techniques for writing effects in ClojureScript. We’ll look at how to separate effects from pure functions, discuss the tradeoffs of tools like callbacks, Promises, and core.async, and explore some strategies for simplifying sequences of effects.

[Slides](https://goo.gl/YNRQkz)

## Usage

### Client REPL

#### Terminal

`clj -A:client:rebel -m figwheel.main`

#### inf-clojure

1. Terminal: `clj -A:client -J-Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}"`
1. Emacs: `M-x inf-clojure-connect`
1. Choose localhost and 5555
1. In Emacs REPL: `(require '[figwheel.main.api])`
1. In Emacs REPL: `(figwheel.main.api/start "dev")`
1. In Emacs buffer (unless you already have `inf-clojure` set to autoload): `M-x inf-clojure-minor-mode`

### Server

`clj -A:server`