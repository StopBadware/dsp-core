# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "hashicorp/precise64"
  config.vm.provision :shell, path: "dev-setup/bootstrap.sh"
  #config.vm.provision "ansible" do |ansible|
  #  ansible.playbook = "dev-setup/playbook.yml"
  #end
  config
  config.vm.network :forwarded_port, host: 4568, guest: 5000
  config.vm.network :private_network, ip: "192.168.15.2"
  config.vm.hostname = "dsp-core-test.example.com"
end
