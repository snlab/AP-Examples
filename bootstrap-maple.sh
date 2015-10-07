#!/usr/bin/env bash
sudo apt-get update
sudo apt-get install -y python-software-properties
sudo add-apt-repository -y ppa:chris-lea/node.js
sudo apt-get update
sudo apt-get -y install openjdk-7-jre openjdk-7-jdk
sudo cp /vagrant/java-x86-64.conf /etc/ld.so.conf.d/
sudo ldconfig
sudo apt-get install -y libsqlite3-dev
sudo apt-get install -y ia32-libs
sudo apt-get install -y nodejs
sudo apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10
echo 'deb http://downloads-distro.mongodb.org/repo/debian-sysvinit dist 10gen' | sudo tee /etc/apt/sources.list.d/mongodb.list
sudo apt-get update
sudo apt-get install -y mongodb-org
sudo service mongod start
sudo apt-get install -y git
git clone https://github.com/lmammino/cube-daemons.git && cd cube-daemons && sudo ./install.sh
sudo apt-get install -y ant
mkdir -p /home/vagrant/.maple
cp -r /vagrant/web /home/vagrant/.maple/
cp -r /vagrant/lib /home/vagrant/.maple/
cp -r /vagrant/javadocs /home/vagrant/.maple/
sudo chown -R vagrant:vagrant /home/vagrant/.maple
sudo chown -R vagrant:vagrant /home/vagrant/cube-daemons
printf "\nPATH=/home/vagrant/.maple/lib:$PATH\n" >> /home/vagrant/.profile

