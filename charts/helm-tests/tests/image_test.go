package tests

import (
	"slices"
	"strings"
	"testing"

	"github.com/gruntwork-io/terratest/modules/helm"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
)

func TestImages_Default(t *testing.T) {
	opts := BaseHelmOptions()

	chart := renderChart(t, opts)
	images := findAllImages(chart)
	assert.ElementsMatch(t, images, []string{
		"airbyte/connector-builder-server:dev",
		"airbyte/cron:dev",
		"bitnami/kubectl:1.28.9",
		"airbyte/server:dev",
		"temporalio/auto-setup:1.23.0",
		"airbyte/webapp:dev",
		"airbyte/worker:dev",
		"airbyte/workload-api-server:dev",
		"airbyte/workload-launcher:dev",
		"airbyte/bootloader:dev",
		"airbyte/mc:latest",
		"busybox:latest",
		"airbyte/db:dev",
		"minio/minio:RELEASE.2023-11-20T22-40-07Z",
	})
}

func TestImages_DefaultAllEnabled(t *testing.T) {
	opts := BaseHelmOptions()
	enableAllImages(opts)

	chart := renderChart(t, opts)
	images := findAllImages(chart)
	assert.ElementsMatch(t, images, []string{
		"airbyte/connector-builder-server:dev",
		"airbyte/cron:dev",
		"bitnami/kubectl:1.28.9",
		"airbyte/server:dev",
		"temporalio/auto-setup:1.23.0",
		"airbyte/webapp:dev",
		"airbyte/worker:dev",
		"airbyte/workload-api-server:dev",
		"airbyte/workload-launcher:dev",
		"airbyte/bootloader:dev",
		"airbyte/mc:latest",
		"busybox:latest",
		"airbyte/db:dev",
		"minio/minio:RELEASE.2023-11-20T22-40-07Z",
		"airbyte/featureflag-server:dev",
		"airbyte/metrics-reporter:dev",
		"temporalio/ui:2.30.1",
	})
}

func TestImages_GlobalTag(t *testing.T) {
	opts := BaseHelmOptions()
	enableAllImages(opts)

	opts.SetValues["global.image.tag"] = "test-tag"
	chart := renderChart(t, opts)
	images := findAllImages(chart)
	assert.ElementsMatch(t, images, []string{
		"airbyte/connector-builder-server:test-tag",
		"airbyte/cron:test-tag",
		"bitnami/kubectl:1.28.9",
		"airbyte/server:test-tag",
		"temporalio/auto-setup:1.23.0",
		"airbyte/webapp:test-tag",
		"airbyte/worker:test-tag",
		"airbyte/workload-api-server:test-tag",
		"airbyte/workload-launcher:test-tag",
		"airbyte/bootloader:test-tag",
		"airbyte/mc:latest",
		"busybox:latest",
		"airbyte/db:test-tag",
		"minio/minio:RELEASE.2023-11-20T22-40-07Z",
		"airbyte/featureflag-server:test-tag",
		"airbyte/metrics-reporter:test-tag",
		"temporalio/ui:2.30.1",
	})
}

func TestImages_GlobalRegistry(t *testing.T) {
	opts := BaseHelmOptions()
	enableAllImages(opts)

	reg := "http://my-registry/"
	opts.SetValues["global.image.registry"] = reg

	chart := renderChart(t, opts)

	for _, img := range findAllImages(chart) {
		if !strings.HasPrefix(img, reg) {
			t.Errorf("%s does not have the registry prefix", img)
		}
	}

	// Some images show up in the env config map,
	// e.g. images that are getting passed into the workload-launcher.
	env := getConfigMap(chart, "airbyte-airbyte-env")
	for k, v := range env.Data {
		if strings.HasSuffix(k, "_IMAGE") {
			if !strings.HasPrefix(v, reg) {
				t.Errorf("env var %s=%q does not have the registry prefix", k, v)
			}
		}
	}

	// The loop above checks these too, but these are important core images, so they're tested explicitly here.
	assert.Equal(t, "http://my-registry/busybox:1.35", env.Data["JOB_KUBE_BUSYBOX_IMAGE"])
	assert.Equal(t, "http://my-registry/curlimages/curl:7.83.1", env.Data["JOB_KUBE_CURL_IMAGE"])
	assert.Equal(t, "http://my-registry/airbyte/container-orchestrator:dev", env.Data["CONTAINER_ORCHESTRATOR_IMAGE"])
	assert.Equal(t, "http://my-registry/airbyte/connector-sidecar:dev", env.Data["CONNECTOR_SIDECAR_IMAGE"])
	assert.Equal(t, "http://my-registry/airbyte/workload-init-container:dev", env.Data["WORKLOAD_INIT_IMAGE"])
}

func TestImages_AppTag(t *testing.T) {
	opts := BaseHelmOptions()
	enableAllImages(opts)

	// This is here to demonstrate that the app image tags
	// take precendence over the global tag
	opts.SetValues["global.image.tag"] = "global-tag"

	moreApps := []string{
		"postgresql", "minio", "testWebapp", "temporal-ui",
		"featureflag-server",
	}
	for _, app := range slices.Concat(allApps, moreApps) {
		setAppOpt(opts, app, "image.tag", "app-tag")
	}
	opts.SetValues["minio.mcImage.tag"] = "mc-app-tag"

	chart := renderChart(t, opts)
	images := findAllImages(chart)
	assert.ElementsMatch(t, images, []string{
		"airbyte/connector-builder-server:app-tag",
		"airbyte/cron:app-tag",
		"bitnami/kubectl:app-tag",
		"airbyte/server:app-tag",
		"temporalio/auto-setup:app-tag",
		"airbyte/webapp:app-tag",
		"airbyte/worker:app-tag",
		"airbyte/workload-api-server:app-tag",
		"airbyte/workload-launcher:app-tag",
		"airbyte/bootloader:app-tag",
		"airbyte/mc:mc-app-tag",
		"busybox:app-tag",
		"airbyte/db:app-tag",
		"minio/minio:app-tag",
		"airbyte/featureflag-server:app-tag",
		"airbyte/metrics-reporter:app-tag",
		"temporalio/ui:app-tag",
	})
}

func TestImages_PullSecrets(t *testing.T) {
	// If global.imagePullSecrets is set, then all pods should use it.
	opts := BaseHelmOptions()
	enableAllImages(opts)

	opts.SetValues["global.imagePullSecrets[0].name"] = "test-img-pull-secret-1"
	opts.SetValues["global.imagePullSecrets[1].name"] = "test-img-pull-secret-2"
	chart := renderChart(t, opts)

	objs := decodeK8sResources(chart)
	for _, obj := range objs {

		name := getK8sObjName(obj)
		podSpec := getPodSpec(obj)
		if podSpec == nil {
			continue
		}

		t.Run(name, func(t *testing.T) {
			assert.ElementsMatch(t, podSpec.ImagePullSecrets, []corev1.LocalObjectReference{
				{Name: "test-img-pull-secret-1"},
				{Name: "test-img-pull-secret-2"},
			})
		})
	}
}

func TestImages_StringImages(t *testing.T) {
	// Make sure the imageUrl helper handles the cases where the image value
	// is a string instead of an object.
	opts := BaseHelmOptions()
	opts.SetValues["global.jobs.kube.images.busybox"] = "my-busybox"
	opts.SetValues["global.jobs.kube.images.curl"] = "my-curl"
	opts.SetValues["workload-launcher.containerOrchestrator.image"] = "my-oc"
	opts.SetValues["workload-launcher.connectorSidecar.image"] = "my-cs"
	opts.SetValues["workload-launcher.workloadInit.image"] = "my-wi"

	chart := renderChart(t, opts)
	env := getConfigMap(chart, "airbyte-airbyte-env")
	assert.Equal(t, "my-busybox", env.Data["JOB_KUBE_BUSYBOX_IMAGE"])
	assert.Equal(t, "my-curl", env.Data["JOB_KUBE_CURL_IMAGE"])
	assert.Equal(t, "my-oc", env.Data["CONTAINER_ORCHESTRATOR_IMAGE"])
	assert.Equal(t, "my-cs", env.Data["CONNECTOR_SIDECAR_IMAGE"])
	assert.Equal(t, "my-wi", env.Data["WORKLOAD_INIT_IMAGE"])
}

func enableAllImages(opts *helm.Options) {
	opts.SetValues["metrics.enabled"] = "true"
	opts.SetValues["featureflag-server.enabled"] = "true"
	opts.SetValues["temporal-ui.enabled"] = "true"
}

func findAllImages(chartYaml string) []string {
	objs := decodeK8sResources(chartYaml)
	images := []string{}
	for _, obj := range objs {

		podSpec := getPodSpec(obj)
		if podSpec == nil {
			continue
		}

		for _, c := range podSpec.InitContainers {
			images = append(images, c.Image)
		}
		for _, c := range podSpec.Containers {
			images = append(images, c.Image)
		}
	}
	return images
}

// dropImageTag drops the tag from a docker image,
// e.g. dropImageTag("airbyte-bootloader:0.64.7") returns "airbyte-bootloader".
//
// The way the charts are currently organized (in subcharts pinned to released versions),
// we can't easily test code that relies on .Chart.AppVersion such as the images code,
// so this allows test code to match the image name without the tag at least.
func dropImageTag(x string) string {
	idx := strings.LastIndex(x, ":")
	if idx == -1 {
		return x
	}
	return x[:idx]
}
