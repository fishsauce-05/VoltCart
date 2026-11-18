import type { NextConfig } from "next";

/** The pictures are served by the Spring Boot backend; the API address can be changed with NEXT_PUBLIC_API. */
const nextConfig: NextConfig = { reactStrictMode: true, devIndicators: false, agentRules: false };

export default nextConfig;
