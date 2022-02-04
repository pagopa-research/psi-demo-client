package it.lockless.psidemoclient.util;

import psi.client.PsiClientKeyDescription;

/**
 * This class holds the clientKeyDescription object written as yaml file and can be considered
 * This class is decoupled from the ClientKeyDescription class of the psi-sdk to provide more flexibility
 */

public class PsiClientKeyDescriptionYaml {

    private String clientPrivateKey;
    private String serverPublicKey;
    private String modulus;

    private String ecClientPrivateKey;
    private String ecServerPublicKey;
    private String ecSpecName;

    public PsiClientKeyDescriptionYaml() {
    }

    public PsiClientKeyDescriptionYaml(PsiClientKeyDescription psiClientKeyDescription) {
        this.clientPrivateKey = psiClientKeyDescription.getClientPrivateKey();
        this.serverPublicKey = psiClientKeyDescription.getServerPublicKey();
        this.modulus = psiClientKeyDescription.getModulus();
        this.ecClientPrivateKey = psiClientKeyDescription.getEcClientPrivateKey();
        this.ecServerPublicKey = psiClientKeyDescription.getEcServerPublicKey();
        this.ecSpecName = psiClientKeyDescription.getEcSpecName();
    }

    public String getClientPrivateKey() {
        return clientPrivateKey;
    }

    public void setClientPrivateKey(String clientPrivateKey) {
        this.clientPrivateKey = clientPrivateKey;
    }

    public String getServerPublicKey() {
        return serverPublicKey;
    }

    public void setServerPublicKey(String serverPublicKey) {
        this.serverPublicKey = serverPublicKey;
    }

    public String getModulus() {
        return modulus;
    }

    public void setModulus(String modulus) {
        this.modulus = modulus;
    }

    public String getEcClientPrivateKey() {
        return ecClientPrivateKey;
    }

    public void setEcClientPrivateKey(String ecClientPrivateKey) {
        this.ecClientPrivateKey = ecClientPrivateKey;
    }

    public String getEcServerPublicKey() {
        return ecServerPublicKey;
    }

    public void setEcServerPublicKey(String ecServerPublicKey) {
        this.ecServerPublicKey = ecServerPublicKey;
    }

    public String getEcSpecName() {
        return ecSpecName;
    }

    public void setEcSpecName(String ecSpecName) {
        this.ecSpecName = ecSpecName;
    }

    @Override
    public String toString() {
        return "PsiClientKeyDescriptionYaml{" +
                "clientPrivateKey='" + clientPrivateKey + '\'' +
                ", serverPublicKey='" + serverPublicKey + '\'' +
                ", modulus='" + modulus + '\'' +
                ", ecClientPrivateKey='" + ecClientPrivateKey + '\'' +
                ", ecServerPublicKey='" + ecServerPublicKey + '\'' +
                ", ecSpecName='" + ecSpecName + '\'' +
                '}';
    }
}
