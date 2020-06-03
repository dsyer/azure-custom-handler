FROM spring-graal-native-dev:20.1-dev-java8

USER root

RUN mkdir -m 0755 /nix && chown 1000:1000 /nix

USER 1000:1000

RUN git clone https://github.com/dsyer/nix-config \$HOME/nix-config && ln -s \$HOME/nix-config/home/.config/* \$HOME/.config

RUN curl https://nixos.org/nix/install | sh