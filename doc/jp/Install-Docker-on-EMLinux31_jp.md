# DockerをEMLinux 3.1にインストールする
本記事では、EMLinux 3.1にDockerをインストールするための手順を示します。

## 目次
- [構成図](#構成図)
- [各ソフトウェアのバージョン](#各ソフトウェアのバージョン)
- [前提条件](#前提条件)
- [EMLinux 3.1のビルドに必要なパッケージのインストール](#emlinux-31のビルドに必要なパッケージのインストール)
- [meta-emlinuxリポジトリのクローン](#meta-emlinuxリポジトリのクローン)
- [レシピのカスタマイズ](#レシピのカスタマイズ)
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
| | | +--------------------+ | | |
| | | | +----------------+ | | | |
| | | | | NGINX          | | | | |
| | | | +----------------+ | | | |
| | | | Docker             | | | |
| | | +--------------------+ | | |
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
1. 以下を参照してください。
   - [各ソフトウェアのバージョン](./Build-EMLinux31_jp.md#各ソフトウェアのバージョン)

## 前提条件
1. 以下を参照してください。
   - [前提条件](./Build-EMLinux31_jp.md#前提条件)

## EMLinux 3.1のビルドに必要なパッケージのインストール
1. 以下を参考に、必要なパッケージをインストールしてください。
   - [EMLinux 3.1のビルドに必要なパッケージのインストール](../../doc/jp/Build-EMLinux31_jp.md#emlinux-31のビルドに必要なパッケージのインストール)

## meta-emlinuxリポジトリのクローン
1. リポジトリをクローンするためのディレクトリを作成してください。
   ```sh
   mkdir -p /home/emlinux/github/emlinux/bookworm/docker/repos
   ```
1. 以下のディレクトリに移動してください。   
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/docker
   ```   
1. git cloneコマンドで、リポジトリをクローンしてください。
   ```sh
   git clone -b bookworm https://github.com/miraclelinux/meta-emlinux.git repos/meta-emlinux
   ```

## レシピのカスタマイズ
1. 本レポジトリをクローンするため、任意のディレクトリに移動してください。
   ```sh
   cd /home/emlinux/github
   ```
1. git cloneコマンドで、本レポジトリをクローンしてください。
   ```sh
   git clone https://github.com/takaaki-fukunaga/EMLinux.git
   ```
1. [meta-docker](../../layer/meta-docker/)ディレクトリを、meta-emlinuxをクローンしたディレクトリに移動してください。
   ```sh
   cp -a EMLinux/layer/meta-docker /home/emlinux/github/emlinux/bookworm/docker/repos/
   ```
1. vimコマンドで、http-proxy.confファイルを開いてください。
   ```sh
   vim /home/emlinux/github/emlinux/bookworm/docker/repos/meta-docker/recipes-docker/configure-docker/files/http-proxy.conf
   ```
1. http-proxy.confを環境に合わせて編集してください。
   ```
   [Service]
   Environment="HTTP_PROXY=http://your-proxy-server.com:80"
   Environment="HTTPS_PROXY=http://your-proxy-server.com:80"
   ```

## EMLinuxのビルド
1. 以下のディレクトリに移動してください。
   ```sh
   cd /home/emlinux/github/emlinux/bookworm/docker/repos/meta-emlinux/docker
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
1. 以下のコマンドを実行し、meta-dockerが追加されていることを確認してください。
   ```sh
   $ bitbake-layers show-layers 
   NOTE: Starting bitbake server...
   layer                 path                                      priority
   ==========================================================================
   meta                  /home/build/work/build/../repos/isar/meta  5
   meta-emlinux          /home/build/work/build/../repos/meta-emlinux  12
   isar-cip-core         /home/build/work/build/../repos/isar-cip-core  6
   meta-docker           /home/build/work/build/../repos/meta-docker  30
   ```
1. viエディタでlocal.confファイルを開き、ファイルの末尾に以下を追記してください。
   ```
   $ vi conf/local.conf
   (snip)
   # ARM64
   MACHINE = "qemu-arm64"
   
   # Setup for Docker
   IMAGE_INSTALL:append = " configure-docker"
   
   # Extra space for rootfs in MB
   ROOTFS_EXTRA = "10240"
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
   sudo apt install -y qemu-system-arm seabios
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
1. IPアドレスが割り振られていることを確認してください。
   ```
   root@EMLinux3:~# ip a
   (snip)
   2: enp0s1: <BROADCAST,MULTICAST,DYNAMIC,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP group default qlen 1000
    link/ether 52:54:00:12:34:56 brd ff:ff:ff:ff:ff:ff
    inet 192.168.122.77/24 metric 1024 brd 192.168.122.255 scope global dynamic enp0s1
       valid_lft 3523sec preferred_lft 3523sec
   (snip)
   ```
1. コンテナイメージの検索が行えるか確認してください。
   ```
   root@EMLinux3:~# docker search nginx 
   NAME                                              DESCRIPTION                                     STARS     OFFICIAL   AUTOMATED
   nginx                                             Official build of Nginx.                        19768     [OK]       
   (snip)
   ```
1. 以下のコマンドを実行し、NGINXのコンテナを起動してください。
   ```sh
   docker run -it -d --name nginx-test -p 80:80 nginx:latest
   ```
1. コンテナホスト (本記事ではUbuntu Server) からcurlコマンドで、NGINXにアクセスできることを確認してください。
   ```
   $ curl 192.168.122.77 --noproxy 192.168.122.77
   <!DOCTYPE html>
   <html>
   <head>
   <title>Welcome to nginx!</title>
   (snip)
   ```
