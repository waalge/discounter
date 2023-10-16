{
  description = "A clj-nix flake";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-23.05";
    flake-utils.url = "github:numtide/flake-utils";
    clj-nix.url = "github:jlesquembre/clj-nix";
  };

  outputs = { self, nixpkgs, flake-utils, clj-nix }:

    flake-utils.lib.eachDefaultSystem (system: 
      let
        overlay = final : prev : { 
          # FIXME : https://github.com/jlesquembre/clj-nix/issues/94
          # Attempted to bump the version but ran into other issues
          # Pinned nixpkgs to 23.05 instead
        };
        pkgs = nixpkgs.legacyPackages.${system}.extend overlay;

      in {

      packages = {
        default = clj-nix.lib.mkCljApp {
          inherit pkgs;
          modules = [
            # Option list:
            # https://jlesquembre.github.io/clj-nix/options/
            {
              projectSrc = ./.;
              name = "pricer";
              main-ns = "pricer.core";
              nativeImage.enable = true;
              # customJdk.enable = true;
            }
          ];
        };
      };
      devShells.default = pkgs.mkShell {
        packages = with pkgs; [
          clojure
          clojure-lsp
          leiningen
        ];
      };
    });
}
