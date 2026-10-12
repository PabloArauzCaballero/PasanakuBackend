import glob,re,collections,os
t=collections.defaultdict(lambda:[0,0,0,0])
for f in glob.glob('**/build/test-results/*/*.xml',recursive=True):
    h=open(f,encoding='utf8',errors='ignore').read(600)
    m=re.search(r'tests="(\d+)" skipped="(\d+)" failures="(\d+)" errors="(\d+)"',h)
    if not m: continue
    n=f.replace(os.sep,'/')
    svc=n.split('/build/')[0].split('/')[-1]; task=n.split('test-results/')[1].split('/')[0]
    for i in range(4): t[(svc,task)][i]+=int(m.group(i+1))
tot=[0]*4
for k,v in sorted(t.items()):
    for i in range(4): tot[i]+=v[i]
    if v[1] or v[2] or v[3]: print('ROJO/OMIT',k,v)
print('grupos',len(t),'tests/skip/fail/err',tot)
for s in ['inversiones','nucleo-financiero','identidad','grupos']:
    print(s,{k[1]:v[0] for k,v in t.items() if k[0]==s})
