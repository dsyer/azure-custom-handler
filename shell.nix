with import <nixpkgs> { };
mkShell {
  name = "env";
  buildInputs = [ azure-cli azure-func figlet ];
  shellHook = ''
    figlet ":azure func:"
  '';
}
