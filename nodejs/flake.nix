{
  description = "Nodejs environment";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { nixpkgs, ... }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
      in
    {
      devShells.${system} = {
        default = pkgs.mkShell {
          packages = with pkgs; [ nodejs typescript-language-server ];
        };
      };
    };
}
