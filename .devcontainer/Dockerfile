FROM springci/spring-native:dev-java11-0.11.x

USER root


ARG USER=vscode
ARG USER_ID=1000
ARG USER_GID=1000

RUN mkdir -m 0755 /nix && chown ${USER_ID}:${USER_GID} /nix

RUN (groupadd --gid "${USER_GID}" "${USER}" || echo "No groupadd needed") && \
    useradd \
      --uid ${USER_ID} \
      --gid ${USER_GID} \
      --create-home \
      --shell /bin/bash \
      ${USER}

USER ${USER_ID}:${USER_GID}

RUN bash -c 'mkdir -p $HOME/.config && git clone https://github.com/dsyer/nix-config $HOME/nix-config && ln -s $HOME/nix-config/home/.config/* $HOME/.config'

RUN curl -L https://nixos.org/nix/install | sh