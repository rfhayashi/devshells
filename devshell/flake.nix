{
  description = "Command line utility to manage devshells";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
    in
      {
        devShells.${system} = {
          default = pkgs.mkShell {
            packages = with pkgs; [ babashka clojure-lsp ];
          };
        };
      };
}
