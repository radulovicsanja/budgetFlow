type Point = { label: string; income: number; expenses: number }

const COLORS = ['#0f6b4c', '#c9782a', '#1f8f68', '#8a4b00', '#3d6b5a', '#b42318', '#5c7268', '#23483a']

function niceMax(raw: number): number {
  if (!Number.isFinite(raw) || raw <= 0) return 0
  if (raw <= 10) return Math.ceil(raw)
  const pow = Math.pow(10, Math.floor(Math.log10(raw)))
  const n = raw / pow
  const nice = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10
  return nice * pow
}

  monthLabel,
  income,
  expenses,
}: {
  monthLabel: string
  income: number
  expenses: number
}) {
  const w = 360
  const h = 200
  const padT = 28
  const padB = 40
  const padL = 48
  const padR = 20
  const rawMax = Math.max(income, expenses, 0)
  const max = niceMax(rawMax) || 1
  const allZero = income <= 0 && expenses <= 0

  const barW = 56
  const gap = 48
  const baseY = h - padB
  const chartH = h - padT - padB
  const x0 = padL + 40
  const x1 = x0 + barW + gap

  const hIncome = allZero ? 0 : (income / max) * chartH
  const hExpenses = allZero ? 0 : (expenses / max) * chartH

  return (
    <div>
      {!allZero ? null : (
        <p className="muted" style={{ marginBottom: '0.35rem', fontSize: '0.85rem' }}>
          Za {monthLabel} nema budžeta ni troškova (prihod i trošak = 0 €).
        </p>
      )}
      <svg viewBox={`0 0 ${w} ${h}`} className="chart-svg" role="img" aria-label={`Mjesec ${monthLabel}`}>
        {[0, 0.5, 1].map((t) => {
          const val = (allZero ? 0 : max) * t
          const yy = baseY - t * chartH
          return (
            <g key={t}>
              <line x1={padL} y1={yy} x2={w - padR} y2={yy} className="chart-grid" />
              <text x={padL - 6} y={yy + 3} textAnchor="end" className="chart-label">
                {allZero ? '0' : val >= 1000 ? `${(val / 1000).toFixed(1)}k` : val.toFixed(0)}
              </text>
            </g>
          )
        })}
        <rect
          x={x0}
          y={baseY - hIncome}
          width={barW}
          height={hIncome}
          className="chart-bar income"
          rx={4}
        />
        <rect
          x={x1}
          y={baseY - hExpenses}
          width={barW}
          height={hExpenses}
          className="chart-bar expenses"
          rx={4}
        />
        {/* nula-markeri kad nema visine */}
        {hIncome < 2 && (
          <line x1={x0} y1={baseY} x2={x0 + barW} y2={baseY} className="chart-line income" strokeWidth={3} />
        )}
        {hExpenses < 2 && (
          <line x1={x1} y1={baseY} x2={x1 + barW} y2={baseY} className="chart-line expenses" strokeWidth={3} />
        )}
        <text x={x0 + barW / 2} y={h - 14} textAnchor="middle" className="chart-label">
          Prihodi
        </text>
        <text x={x1 + barW / 2} y={h - 14} textAnchor="middle" className="chart-label">
          Troškovi
        </text>
        <text x={x0 + barW / 2} y={baseY - hIncome - 8} textAnchor="middle" className="chart-legend income">
          {income.toFixed(0)} €
        </text>
        <text x={x1 + barW / 2} y={baseY - hExpenses - 8} textAnchor="middle" className="chart-legend expenses">
          {expenses.toFixed(0)} €
        </text>
      </svg>
    </div>
  )
}

export function LineChart({
  data,
  highlightLabel,
}: {
  data: Point[]
  highlightLabel?: string
}) {
  const w = 520
  const h = 240
  const padL = 48
  const padR = 20
  const padT = 28
  const padB = 36

  const hasData = data.length > 0
  const rawMax = hasData ? Math.max(0, ...data.flatMap((d) => [d.income, d.expenses])) : 0
  const allZero = hasData && rawMax <= 0
  const max = niceMax(rawMax)

  if (!hasData || allZero) {
    return (
      <div className="chart-empty">
        <p className="muted">
          {allZero
            ? 'Nema prihoda ni troškova u prikazanom periodu — sve je 0.'
            : 'Nema podataka za grafikon.'}
        </p>
        {allZero && (
          <svg viewBox={`0 0 ${w} ${h}`} className="chart-svg" role="img" aria-label="Prazan trend">
            <line x1={padL} y1={h - padB} x2={w - padR} y2={h - padB} className="chart-grid" />
            {data.map((d, i) => {
              const x = padL + (i * (w - padL - padR)) / Math.max(data.length - 1, 1)
              const active = d.label === highlightLabel
              return (
                <text
                  key={d.label}
                  x={x}
                  y={h - 10}
                  textAnchor="middle"
                  className={active ? 'chart-label chart-label-active' : 'chart-label'}
                >
                  {d.label.slice(5)}
                </text>
              )
            })}
            <text x={padL} y={16} className="chart-legend">
              0 €
            </text>
          </svg>
        )}
      </div>
    )
  }

  const x = (i: number) => padL + (i * (w - padL - padR)) / Math.max(data.length - 1, 1)
  const y = (v: number) => h - padB - (v / max) * (h - padT - padB)

  const line = (key: 'income' | 'expenses') =>
    data.map((d, i) => `${i === 0 ? 'M' : 'L'} ${x(i)} ${y(d[key])}`).join(' ')

  const ticks = [0, 0.25, 0.5, 0.75, 1]

  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="chart-svg" role="img" aria-label="Trend prihoda i rashoda">
      {ticks.map((t) => {
        const val = max * t
        const yy = y(val)
        return (
          <g key={t}>
            <line x1={padL} y1={yy} x2={w - padR} y2={yy} className="chart-grid" />
            <text x={padL - 6} y={yy + 3} textAnchor="end" className="chart-label">
              {val >= 1000 ? `${(val / 1000).toFixed(1)}k` : val.toFixed(0)}
            </text>
          </g>
        )
      })}
      <path d={line('income')} className="chart-line income" fill="none" />
      <path d={line('expenses')} className="chart-line expenses" fill="none" />
      {data.map((d, i) => {
        const active = d.label === highlightLabel
        return (
          <g key={d.label}>
            {active && (
              <line
                x1={x(i)}
                y1={padT}
                x2={x(i)}
                y2={h - padB}
                className="chart-highlight"
              />
            )}
            <circle cx={x(i)} cy={y(d.income)} r={active ? 5 : 3.5} className="chart-dot income">
              <title>
                {d.label}: prihod {d.income.toFixed(2)} €
              </title>
            </circle>
            <circle cx={x(i)} cy={y(d.expenses)} r={active ? 5 : 3.5} className="chart-dot expenses">
              <title>
                {d.label}: trošak {d.expenses.toFixed(2)} €
              </title>
            </circle>
            <text
              x={x(i)}
              y={h - 10}
              textAnchor="middle"
              className={active ? 'chart-label chart-label-active' : 'chart-label'}
            >
              {d.label.slice(5)}
            </text>
          </g>
        )
      })}
      <text x={padL} y={16} className="chart-legend income">
        Prihodi
      </text>
      <text x={padL + 80} y={16} className="chart-legend expenses">
        Rashodi
      </text>
    </svg>
  )
}

export function PieChart({
  data,
}: {
  data: { name: string; value: number }[]
}) {
  const filtered = data.filter((d) => d.value > 0)
  const total = filtered.reduce((s, d) => s + d.value, 0)
  if (total <= 0) {
    return <p className="muted">Nema potrošnje za ovaj mjesec.</p>
  }

  const size = 200
  const r = 70
  const cx = 100
  const cy = 100
  let angle = -Math.PI / 2

  const slices = filtered.map((d, i) => {
    const slice = (d.value / total) * Math.PI * 2
    const start = angle
    angle += slice
    const end = angle
    const large = slice > Math.PI ? 1 : 0
    const x1 = cx + r * Math.cos(start)
    const y1 = cy + r * Math.sin(start)
    const x2 = cx + r * Math.cos(end)
    const y2 = cy + r * Math.sin(end)
    const path = `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${large} 1 ${x2} ${y2} Z`
    return { ...d, path, color: COLORS[i % COLORS.length], pct: Math.round((d.value / total) * 100) }
  })

  return (
    <div className="pie-wrap">
      <svg viewBox={`0 0 ${size} ${size}`} className="chart-svg pie" role="img" aria-label="Potrošnja po kategorijama">
        {slices.map((s) => (
          <path key={s.name} d={s.path} fill={s.color} />
        ))}
      </svg>
      <ul className="pie-legend">
        {slices.map((s) => (
          <li key={s.name}>
            <span className="swatch" style={{ background: s.color }} />
            {s.name} — {s.pct}% ({s.value.toFixed(2)} €)
          </li>
        ))}
      </ul>
    </div>
  )
}
