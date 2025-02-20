{
  outputs = { ... } : {
    templates = {
      blank = {
        path = ./blank;
        description = "Blank development environment";
      };
      clojure = {
        path = ./clojure;
        description = "Clojure development environment";
      };
    };
  };
}
