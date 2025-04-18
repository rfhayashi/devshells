{
  description = "Clojure development environment";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
      in
    {
      devShells.${system} = {
        default = pkgs.mkShell {
          packages = with pkgs; [ clojure clojure-lsp leiningen ];
        };
      };
    };
}
