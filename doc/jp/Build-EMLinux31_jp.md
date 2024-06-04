# EMLinux 3.1のビルド方法
本記事では、GitHub版のEMLinux 3.1をDockerを活用しビルドする手順を示します。また、ビルドしたEMLinux 3.1のOSイメージ (ARM64版) をQEMU (qemu-system-aarch64) で起動します。

## 目次
- [構成図](#構成図)
- [各ソフトウェアのバージョン](#各ソフトウェアのバージョン)
- [前提条件](#前提条件)
- [EMLinux 3.1のビルドに必要なパッケージのインストール](#emlinux-31のビルドに必要なパッケージのインストール)
- [meta-emlinuxリポジトリのクローン](#meta-emlinuxリポジトリのクローン)
- [EMLinuxのビルド](#emlinuxのビルド)
- [QEMUでEMLinux 3.1を起動する](#qemuでemlinux-31を起動する)

## 構成図
### EMLinux 3.1のビルド時
```
+--------------------------------+
| +----------------------------+ |
| | +------------------------+ | |
| | | +--------------------+ | | |
| | | | Build EMLinux 3.1  | | | |
| | | +--------------------+ | | |
| | | Debian                 | | |
| | +------------------------+ | |
| | Docker                     | |
| +----------------------------+ |
| Ubuntu Server                  |
+--------------------------------+
```

### EMLinux 3.1の起動時
```
+--------------------------------+
| +----------------------------+ |
| | +------------------------+ | |
| | |  EMLinux 3.1           | | |
| | +------------------------+ | |
| | QEMU                       | |
| +-+--------------------------+ |
|   |                            |
| +-+----+                       |
| | tap0 |                       |
| +-+----+                       |
|   |                            |
| +-+----------------------+     |
| | virbr0 (192.168.122.1) |     |
| +------------------------+     |
|                                |
| Ubuntu Server                  |
+--------------------------------+
```

## 各ソフトウェアのバージョン
### Ubuntu Server
```
$ cat /etc/lsb-release 
DISTRIB_ID=Ubuntu
DISTRIB_RELEASE=20.04
DISTRIB_CODENAME=focal
DISTRIB_DESCRIPTION="Ubuntu 20.04.6 LTS"
```

### Docker
```
$ docker --version 
Docker version 24.0.5, build 24.0.5-0ubuntu1~20.04.1
```

### Docker Compose
```
$ docker compose version 
Docker Compose version 2.20.2+ds1-0ubuntu1~20.04.1
```

### QEMU
```
$ qemu-system-aarch64 --version 
QEMU emulator version 4.2.1 (Debian 1:4.2-3ubuntu6.28)
Copyright (c) 2003-2019 Fabrice Bellard and the QEMU Project developers
```

## 前提条件
### ハードウェア要件
|項目|要件|
|-|-|
|OS|Dockerが動作するLinux|
|CPU|Intel 64対応プロセッサまたはAMD64対応プロセッサ (8スレッド以上を推奨)|
|RAM|8GiB以上 (16GiB以上を推奨)|
|Disk|30GiB以上の空き容量|
|その他|インターネットに接続可能であること|

### Proxyサーバ
- インターネット接続のためにProxyサーバを使用している場合、以下の設定を行ってください。

#### .bashrc
- .bashrcの末尾に以下を追記してください。
  ```
  PROXY=http://<proxyサーバのIPアドレス>:<ポート番号>
  export http_proxy=${PROXY}
  export https_proxy=${PROXY}
  export ftp_proxy=${PROXY}
  export no_proxy='127.0.0.1' 
  ```

#### Docker
- /etc/systemd/system/docker.service.d/http-proxy.confに以下を追記してください。
  ```
  [Service]
  Environment="HTTP_PROXY=<proxyサーバのIPアドレス>:<ポート番号>"
  Environment="HTTPS_PROXY=<proxyサーバのIPアドレス>:<ポート番号>"
  ```

#### Git
- 以下を実行してください。
  ```sh
  git config --global http.proxy <proxyサーバのFQDNまたはIPアドレス>:<ポート番号>
  ```
  ```sh
  git config --global https.proxy <proxyサーバのFQDNまたはIPアドレス>:<ポート番号>
  ```

### EMLinux 3.1ビルド時
- 一般ユーザでビルド可能です。本記事では、emlinuxという名前のユーザを使用しました。ユーザemlinuxのホームディレクトリは、/home/emlinuxです。

### EMLinux 3.1起動時
- 本記事では、TAPデバイスを利用するため、sudo権限を持っているユーザを用意してください。

## EMLinux 3.1のビルドに必要なパッケージのインストール
1. 以下のパッケージをインストールしてください。
   ```sh
   sudo apt install -y docker.io docker-compose-v2 qemu-user-static
   ```
   - docker.io, docker-compose-v2
     - EMLinux 3.1をビルドするためのDebianのコンテナを作成および起動するために用います。
   - qemu-user-static
     - クロスコンパイルするために用います。本記事では、x86_64のUbuntu Server上で、ARM64版のEMLinux 3.1をビルドするために用います。
       - コンテナホスト: x86_64
       - コンテナ内でビルドするバイナリ: ARM64
1. ユーザemlinuxを、グループdockerに追加してください。
   ```sh
   sudo gpasswd -a emlinux docker
   ```
1. 一度ログアウトし、再ログイン後、sudoなしでdockerコマンドを実行できることを確認してください。
   ```sh
   $ docker ps 
   CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
   ```

## meta-emlinuxリポジトリのクローン
1. リポジトリをクローンするためのディレクトリを作成してください。
   ```sh
   mkdir -p /home/emlinux/github/emlinux/bookworm/misc/repos
   ```
1. 以下のディレクトリに移動してください。   
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/misc
   ```   
1. git cloneコマンドで、リポジトリをクローンしてください。
   ```sh
   git clone -b bookworm https://github.com/miraclelinux/meta-emlinux.git repos/meta-emlinux
   ```
## EMLinuxのビルド
1. 以下のディレクトリに移動してください。
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/misc/repos/meta-emlinux/docker
   ```
1. 上記dockerディレクトリ内にあるrun.shを実行してください。初回起動時にはコンテナの作成処理が行われます。コンテナ作成処理後、コンテナにログインした状態になります。
   ```
   $ ./run.sh
   (snip)
   build@82fa043f8378:~/work$
   ```
1. 以下のコマンドを実行し、ビルドのために必要な環境変数を設定してください。
   ```sh
   source repos/meta-emlinux/scripts/setup-emlinux build 
   ```
1. viエディタでlocal.confファイルを開き、ファイルの末尾に以下を追記してください。
   ```
   $ vi conf/local.conf
   (snip)
   MACHINE = "qemu-arm64"
   ```
1. EMLinuxのビルドを行ってください。
   ```sh
   bitbake emlinux-image-base
   ```
1. ビルド完了後、exitコマンドでコンテナからログアウトしてください。
   ```
   build@82fa043f8378:~/work$ exit 
   exit
   ```
## QEMUでEMLinux 3.1を起動する
1. 以下のパッケージをインストールしてください。
   ```sh
   sudo apt install -y qemu-system-arm seabios libvirt-daemon-system
   ```
1. TAPデバイス (以下の実行例ではtap0) を作成してください。
   ```sh
   sudo ip tuntap add tap0 mode tap
   ```
   ```sh
   sudo ip link set tap0 promisc on
   ```
   ```sh
   sudo ip link set dev tap0 master virbr0
   ```
   ```sh
   sudo ip link set dev tap0 up
   ```
1. 以下のディレクトリに移動してください。
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/misc/build
   ```
1. run.shを作成してください。
   ```sh
   touch run.sh
   ```
   ```sh
   chmod +x run.sh
   ```
1. vimなどでrun.shを以下のように編集してください。
   ```
   qemu-system-aarch64 \
   -net nic \
   -net tap,ifname=tap0,script=no \
   -drive id=disk0,file=./tmp/deploy/images/qemu-arm64/emlinux-image-base-emlinux-bookworm-qemu-arm64.ext4,if=none,format=raw \
   -device virtio-blk-device,drive=disk0 -show-cursor -device VGA,edid=on \
   -device qemu-xhci \
   -device usb-tablet \
   -device usb-kbd \
   -object rng-random,filename=/dev/urandom,id=rng0 \
   -device virtio-rng-pci,rng=rng0 \
   -nographic \
   -machine virt \
   -cpu cortex-a57 \
   -m 2G \
   -serial mon:stdio \
   -serial null \
   -kernel ./tmp/deploy/images/qemu-arm64/emlinux-image-base-emlinux-bookworm-qemu-arm64-vmlinux \
   -initrd ./tmp/deploy/images/qemu-arm64/emlinux-image-base-emlinux-bookworm-qemu-arm64-initrd.img \
   -append 'root=/dev/vda rw highres=off console=ttyS0 mem=2G ip=dhcp console=ttyAMA0'
   ```
1. sudo権限を持ったユーザで、以下を実行してください。
   ```sh
   sudo ./run.sh
   ```
   - パスワードを問われる場合には、sudo権限を持ったユーザのパスワードを入力してください。
1. rootユーザでログインしてください。既定のパスワードはrootです。
   ```
   EMLinux3 login: root
   Password: 
   ```
