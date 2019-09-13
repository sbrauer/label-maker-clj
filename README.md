```
npm init
npm install shadow-cljs react react-dom --save
npx shadow-cljs server
```

Visit http://localhost:9630/
Go to `Builds` and start a watch on `main`

Make sure you have environment vars in place:
```
cp .env.dev-sample .env.dev
```

In Clojure repl (in `user` ns): `(start)`

Visit app at http://localhost:3000/index.html

Resources:
- [Fulcro Developers Guide](http://book.fulcrologic.com/fulcro3)
- [ClojureScript cheatsheet](https://cljs.info/cheatsheet/)
