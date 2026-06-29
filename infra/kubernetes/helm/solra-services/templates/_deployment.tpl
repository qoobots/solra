{{/* Solra 微服务 Deployment 模板 */}}
{{- define "solra.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .serviceName }}
  namespace: {{ .namespace | default "solra" }}
  labels:
    app.kubernetes.io/name: {{ .serviceName }}
    app.kubernetes.io/part-of: solra
spec:
  replicas: {{ .replicaCount | default 1 }}
  selector:
    matchLabels:
      app.kubernetes.io/name: {{ .serviceName }}
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ .serviceName }}
        app.kubernetes.io/part-of: solra
      annotations:
        prometheus.io/scrape: "{{ .metrics.enabled | default true }}"
        prometheus.io/port: "{{ .servicePort | default 8080 }}"
        prometheus.io/path: "{{ .metrics.path | default "/actuator/prometheus" }}"
    spec:
      imagePullSecrets:
        - name: ghcr-secret
      containers:
        - name: {{ .serviceName }}
          image: "{{ .image }}:{{ .imageTag }}"
          imagePullPolicy: {{ .pullPolicy | default "IfNotPresent" }}
          ports:
            - containerPort: {{ .servicePort | default 8080 }}
              protocol: TCP
          {{- if .env }}
          env:
            {{- range $k, $v := .env }}
            - name: {{ $k }}
              value: "{{ $v }}"
            {{- end }}
          {{- end }}
          livenessProbe:
            httpGet:
              path: /health/live
              port: {{ .servicePort | default 8080 }}
            initialDelaySeconds: 30
            periodSeconds: 15
          readinessProbe:
            httpGet:
              path: /health/ready
              port: {{ .servicePort | default 8080 }}
            initialDelaySeconds: 10
            periodSeconds: 5
          {{- if .resources }}
          resources:
            {{- toYaml .resources | nindent 12 }}
          {{- end }}
{{- end }}
