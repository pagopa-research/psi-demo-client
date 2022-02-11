package it.lockless.psidemoclient.util;

import psi.PsiClientKeyDescription;

/**
 * This class holds the clientKeyDescription object written as a yaml file.
 * This class is decoupled from the ClientKeyDescription class of the psi-sdk to provide more flexibility.
 */

public class PsiClientKeyDescriptionYaml {

    private String clientPrivateExponent;
    private String serverPublicExponent;
    private String modulus;

    private String ecClientPrivateD;
    private String ecServerPublicQ;

    public PsiClientKeyDescriptionYaml() {
    }

    public PsiClientKeyDescriptionYaml(PsiClientKeyDescription psiClientKeyDescription) {
        this.clientPrivateExponent = psiClientKeyDescription.getClientPrivateExponent();
        this.serverPublicExponent = psiClientKeyDescription.getServerPublicExponent();
        this.modulus = psiClientKeyDescription.getModulus();
        this.ecClientPrivateD = psiClientKeyDescription.getEcClientPrivateD();
        this.ecServerPublicQ = psiClientKeyDescription.getEcServerPublicQ();
    }

    public String getClientPrivateExponent() {
        return clientPrivateExponent;
    }

    public void setClientPrivateExponent(String clientPrivateExponent) {
        this.clientPrivateExponent = clientPrivateExponent;
    }

    public String getServerPublicExponent() {
        return serverPublicExponent;
    }

    public void setServerPublicExponent(String serverPublicExponent) {
        this.serverPublicExponent = serverPublicExponent;
    }

    public String getModulus() {
        return modulus;
    }

    public void setModulus(String modulus) {
        this.modulus = modulus;
    }

    public String getEcClientPrivateD() {
        return ecClientPrivateD;
    }

    public void setEcClientPrivateD(String ecClientPrivateD) {
        this.ecClientPrivateD = ecClientPrivateD;
    }

    public String getEcServerPublicQ() {
        return ecServerPublicQ;
    }

    public void setEcServerPublicQ(String ecServerPublicQ) {
        this.ecServerPublicQ = ecServerPublicQ;
    }

    @Override
    public String toString() {
        return "PsiClientKeyDescriptionYaml{" +
                "clientPrivateExponent='" + clientPrivateExponent + '\'' +
                ", serverPublicExponent='" + serverPublicExponent + '\'' +
                ", modulus='" + modulus + '\'' +
                ", ecClientPrivateD='" + ecClientPrivateD + '\'' +
                ", ecServerPublicQ='" + ecServerPublicQ + '\'' +
                '}';
    }
}
