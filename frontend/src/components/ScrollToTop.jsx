import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

const ScrollToTop = () => {
  const { pathname } = useLocation()

  useEffect(() => {
    // Scroll to top whenever pathname changes
    window.scrollTo({
      top: 0,
      left: 0,
      behavior: 'instant' // Use 'instant' for immediate scroll, 'smooth' for animated
    })

    // Dynamically update rel="canonical" to match the current page URL
    let canonical = document.querySelector('link[rel="canonical"]')
    if (!canonical) {
      canonical = document.createElement('link')
      canonical.setAttribute('rel', 'canonical')
      document.head.appendChild(canonical)
    }
    const currentUrl = `${window.location.origin}${pathname}`
    canonical.setAttribute('href', currentUrl)

    // Update OpenGraph and Twitter URLs for sharing
    const ogUrl = document.querySelector('meta[property="og:url"]')
    if (ogUrl) ogUrl.setAttribute('content', currentUrl)

    const twitterUrl = document.querySelector('meta[name="twitter:url"]')
    if (twitterUrl) twitterUrl.setAttribute('content', currentUrl)
  }, [pathname])

  return null
}

export default ScrollToTop
