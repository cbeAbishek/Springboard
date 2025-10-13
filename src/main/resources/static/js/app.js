document.addEventListener('DOMContentLoaded', () => {
  const toggles = document.querySelectorAll('[data-toggle]');
  toggles.forEach((toggle) => {
    toggle.addEventListener('click', () => {
      const target = document.querySelector(toggle.dataset.toggle);
      if (!target) return;
      target.classList.toggle('hidden');
    });
  });

  const trendContainer = document.querySelector('[data-trend]');
  if (trendContainer) {
    try {
      const trendData = JSON.parse(trendContainer.dataset.trend);
      const labels = Object.keys(trendData);
      const passed = labels.map((label) => trendData[label].passed);
      const failed = labels.map((label) => trendData[label].failed);
      const canvas = document.querySelector('#trend-chart');
      if (canvas && window.Chart) {
        new window.Chart(canvas, {
          type: 'line',
          data: {
            labels,
            datasets: [
              {
                label: 'Passed',
                data: passed,
                borderColor: '#38bdf8',
                tension: 0.4,
              },
              {
                label: 'Failed',
                data: failed,
                borderColor: '#f87171',
                tension: 0.4,
              },
            ],
          },
          options: {
            responsive: true,
            plugins: {
              legend: {
                labels: {
                  color: '#e2e8f0',
                },
              },
            },
            scales: {
              x: {
                ticks: {
                  color: '#94a3b8',
                },
              },
              y: {
                ticks: {
                  color: '#94a3b8',
                },
              },
            },
          },
        });
      }
    } catch (err) {
      console.error('Unable to render trend chart', err);
    }
  }

  setupJsonValidation();
  renderAnalyticsCharts();
});

function setupJsonValidation() {
  const singleForm = document.querySelector('[data-validate="single-test"]');
  if (singleForm) {
    const definitionField = singleForm.querySelector('textarea[name="definitionJson"]');
    const errorNode = singleForm.querySelector('[data-error-for="single-test"]');

    const clearSingleError = () => {
      if (errorNode) {
        errorNode.textContent = '';
        errorNode.classList.remove('visible');
      }
      if (definitionField) {
        definitionField.classList.remove('is-invalid');
      }
    };

    singleForm.addEventListener('submit', (event) => {
      if (singleForm.dataset.validated === 'true') {
        singleForm.dataset.validated = '';
        return;
      }
      if (!definitionField) {
        return;
      }

      clearSingleError();

      const value = definitionField.value.trim();
      if (value.length === 0) {
        event.preventDefault();
        showError(errorNode, 'Provide a JSON definition for the test case.');
        definitionField.classList.add('is-invalid');
        return;
      }

      try {
        JSON.parse(value);
      } catch (err) {
        event.preventDefault();
        showError(errorNode, `Definition JSON is invalid: ${err.message}`);
        definitionField.classList.add('is-invalid');
        return;
      }

      event.preventDefault();
      singleForm.dataset.validated = 'true';
      singleForm.requestSubmit();
    });

    if (definitionField) {
      definitionField.addEventListener('input', clearSingleError);
    }
  }

  const bulkForm = document.querySelector('[data-validate="bulk-import"]');
  if (bulkForm) {
    const fileInput = bulkForm.querySelector('input[type="file"][name="file"]');
    const payloadField = bulkForm.querySelector('textarea[name="payload"]');
    const errorNode = bulkForm.querySelector('[data-error-for="bulk-import"]');

    const clearBulkError = () => {
      if (errorNode) {
        errorNode.textContent = '';
        errorNode.classList.remove('visible');
      }
      if (payloadField) {
        payloadField.classList.remove('is-invalid');
      }
      if (fileInput) {
        fileInput.classList.remove('is-invalid');
      }
    };

    const validateJson = (text) => {
      const trimmed = text.trim();
      if (!trimmed) {
        throw new Error('JSON content is empty');
      }
      JSON.parse(trimmed);
    };

    bulkForm.addEventListener('submit', (event) => {
      if (bulkForm.dataset.validated === 'true') {
        bulkForm.dataset.validated = '';
        return;
      }

      event.preventDefault();
      clearBulkError();

      const hasFile = fileInput && fileInput.files && fileInput.files.length > 0;
      const payloadValue = payloadField ? payloadField.value : '';
      const hasPayload = payloadValue && payloadValue.trim().length > 0;

      if (!hasFile && !hasPayload) {
        showError(errorNode, 'Provide a JSON file or paste a JSON payload for import.');
        if (payloadField) {
          payloadField.classList.add('is-invalid');
        }
        return;
      }

      if (hasPayload) {
        try {
          validateJson(payloadValue);
        } catch (err) {
          showError(errorNode, `Bulk payload JSON is invalid: ${err.message}`);
          if (payloadField) {
            payloadField.classList.add('is-invalid');
          }
          return;
        }
      }

      if (hasFile) {
        const reader = new FileReader();
        reader.onload = (loadEvent) => {
          try {
            validateJson(loadEvent.target.result || '');
            bulkForm.dataset.validated = 'true';
            bulkForm.requestSubmit();
          } catch (err) {
            showError(errorNode, `Uploaded file JSON is invalid: ${err.message}`);
            if (fileInput) {
              fileInput.classList.add('is-invalid');
            }
          }
        };
        reader.onerror = () => {
          showError(errorNode, 'Unable to read the selected file. Please try again.');
          if (fileInput) {
            fileInput.classList.add('is-invalid');
          }
        };
        reader.readAsText(fileInput.files[0]);
        return;
      }

      bulkForm.dataset.validated = 'true';
      bulkForm.requestSubmit();
    });

    if (payloadField) {
      payloadField.addEventListener('input', clearBulkError);
    }
    if (fileInput) {
      fileInput.addEventListener('change', clearBulkError);
    }
  }
}

function showError(node, message) {
  if (!node) {
    console.error(message);
    return;
  }
  node.textContent = message;
  node.classList.add('visible');
}

function renderAnalyticsCharts() {
  const host = document.querySelector('[data-analytics]');
  if (!host || !host.dataset.analytics || !window.Chart) {
    return;
  }

  let analytics;
  try {
    analytics = JSON.parse(host.dataset.analytics);
  } catch (error) {
    console.error('Unable to parse analytics payload', error);
    return;
  }

  const statusCanvas = document.getElementById('status-breakdown');
  if (statusCanvas) {
    const statusEntries = analytics.statusBreakdown ? Object.entries(analytics.statusBreakdown) : [];
    if (statusEntries.length > 0) {
      const statusLabels = statusEntries.map(([label]) => label);
      const statusValues = statusEntries.map(([, value]) => value);
      new window.Chart(statusCanvas, {
        type: 'doughnut',
        data: {
          labels: statusLabels,
          datasets: [
            {
              data: statusValues,
              backgroundColor: buildPalette(statusEntries.length),
              borderWidth: 0,
            },
          ],
        },
        options: defaultChartOptions(),
      });
    }
  }

  const dailyCanvas = document.getElementById('daily-breakdown');
  if (dailyCanvas && Array.isArray(analytics.dailyStats) && analytics.dailyStats.length > 0) {
    const labels = analytics.dailyStats.map((stat) => stat.date);
    const passed = analytics.dailyStats.map((stat) => stat.passed);
    const failed = analytics.dailyStats.map((stat) => stat.failed);
    new window.Chart(dailyCanvas, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Passed',
            data: passed,
            borderColor: '#38bdf8',
            tension: 0.3,
            fill: false,
          },
          {
            label: 'Failed',
            data: failed,
            borderColor: '#f87171',
            tension: 0.3,
            fill: false,
          },
        ],
      },
      options: {
        ...defaultChartOptions(),
        scales: {
          x: {
            ticks: {
              color: '#94a3b8',
            },
          },
          y: {
            ticks: {
              color: '#94a3b8',
            },
          },
        },
      },
    });
  }

  const typeCanvas = document.getElementById('type-breakdown');
  if (typeCanvas && analytics.typeBreakdown) {
    const entries = Object.entries(analytics.typeBreakdown);
    if (entries.length > 0) {
      new window.Chart(typeCanvas, {
        type: 'polarArea',
        data: {
          labels: entries.map(([label]) => label),
          datasets: [
            {
              data: entries.map(([, value]) => value),
              backgroundColor: buildPalette(entries.length),
            },
          ],
        },
        options: defaultChartOptions(),
      });
    }
  }
}

function buildPalette(length) {
  const base = ['#38bdf8', '#34d399', '#facc15', '#f87171', '#a855f7', '#f97316'];
  if (length <= base.length) {
    return base.slice(0, length);
  }
  const colors = [];
  for (let i = 0; i < length; i += 1) {
    colors.push(base[i % base.length]);
  }
  return colors;
}

function defaultChartOptions() {
  return {
    responsive: true,
    plugins: {
      legend: {
        labels: {
          color: '#e2e8f0',
        },
      },
    },
  };
}
