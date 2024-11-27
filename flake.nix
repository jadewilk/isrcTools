{
  description = "Java dev shell";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    build-gradle-application.url = "github:raphiz/buildGradleApplication";
  };

  outputs = { self, nixpkgs, flake-utils, build-gradle-application, ... }:
    let
      pname = "isrcTools";
      version = "0.0.1";
      sys = "x86_64-linux";
      org = "your.org.com";
      # java --enable-preview --source 22 main.java
    in
    flake-utils.lib.eachDefaultSystem (
      system:
      let 
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ build-gradle-application.overlays.default ];
        };
        jdk = pkgs.jdk23.override { enableJavaFX = true; };
        gradle = pkgs.callPackage pkgs.gradle-packages.gradle_8 { java = jdk; };

      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdt-language-server
            java-language-server
            gradle-completion
            updateVerificationMetadata
            jdk
            gradle
          ];
        };
        # Mostly copied from https://nixos.org/manual/nixpkgs/stable/#sec-language-java
        packages = {
            default = self.packages.${sys}.${pname};
            ${pname} = pkgs.buildGradleApplication {
              inherit pname version jdk gradle;
              src = ./.;
              meta.license = pkgs.lib.licenses.mit;
              buildTask = "installDist";
              installLocation = "app/build/install/*/";
        };
      };
    }
  );
}
