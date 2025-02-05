{
  description = "Command line utility to manage devshells";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
      source = ./.;
    in
      {
        devShells.${system} = {
          default = pkgs.mkShell {
            packages = with pkgs; [ babashka clojure-lsp gh ];
          };
        };

        packages.${system} = {
          default = pkgs.writeShellApplication {
            name = "devshell";
            runtimeInputs = [ pkgs.gh ];
            text = ''
              exec ${pkgs.babashka}/bin/bb --config ${source}/bb.edn -m devshell.main "$@"
            '';
          };
        };
      };
}
